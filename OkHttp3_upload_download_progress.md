# OkHttp3上传下载文件，监听进度，极简、高复用
在网上看了很多关使用OkHttp3下载和上传文件监听进度的贴子，千篇一律，用起来还麻烦，所以本人仔仔细细的研究了一下OkHttp3和Okio的API，发现可以充分利用Okio来简化进度监听，话不多说，直接上[源码](https://github.com/mxmhao/Android_App_Template/blob/master/app/src/main/java/template/OkHttp3UploadDownload.java):    
下载：
```
/**
 * 此类使用来包装Response.body().source()
 * Source来自Okio的接口
 */
class ProgressSource implements Source, Progress {

    private Source source;
    private long loadedBytes = 0;//已下载或者已上传的字节数
    public final long totalBytes;//总字节数

    public ProgressSource(Source source, long totalBytes) {
        this.source = source;//Response.body().source()
        this.totalBytes = totalBytes;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        long readCount = source.read(sink, byteCount);//从网络中读取
        if (readCount != -1) loadedBytes += readCount;//读取到的就是已下载的，划重点
        return readCount;
    }

    @Override
    public Timeout timeout() {
        return source.timeout();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    @Override
    public long getLoadedBytes() {
        return loadedBytes;
    }

    @Override
    public long getTotalBytes() {
        return totalBytes;
    }
}

//划重点
private void download() {
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        String url = "http://your.url/xxx.mp4";
        Request request = new Request.Builder().url(url).build();
        ohc.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) return;//失败了
                try {
                    //监控下载进度,包装response.body().source()，划重点
                    ProgressSource source = new ProgressSource(response.body().source(), response.body().contentLength());
                    ProgressTask task = new ProgressTask(source);
                    timer.schedule(task, 1000, 1000);//定时获取进度和速度
                    //文件保存
		    //多并发用RandomAccessFile
                    File file = new File("/sdcard/xxx.mp4");//文件存放位置
                    file.createNewFile();
                    BufferedSink sink = Okio.buffer(Okio.sink(file));//断点续传Okio.appendingSink(file)
                    sink.writeAll(source);//把包装的source写入文件，划重点
                    sink.close();
                    task.cancel();
                } catch (IOException e) {
                } finally {
                }
            }
        });
    }
```
上传：
```
//包装BufferedSink
class ProgressSink implements Sink, Progress {

    private BufferedSink sink;
    private long loadedBytes = 0;//已上传的字节数
    public final long totalBytes;//总字节数

    ProgressSink(BufferedSink sink, long totalBytes) {
        this.sink = sink;
        this.totalBytes = totalBytes;
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        sink.write(source, byteCount);
        loadedBytes += byteCount;
    }

    //此方法就是对RealBufferedSink.writeAll方法改造
    public long writeAll(Source source) throws IOException {
        if (source == null) throw new IllegalArgumentException("source == null");

        long totalBytesRead = 0;
        Buffer buffer = sink.buffer();
        final int size = 8192;//=Segment.SIZE;
        for (long readCount; (readCount = source.read(buffer, size)) != -1; ) {
            sink.emitCompleteSegments();//发射到网络流中
            totalBytesRead += readCount;
            loadedBytes += readCount;//发射完的就是已上传的，划重点
        }
        return totalBytesRead;
    }

    @Override
    public void flush() throws IOException {
        sink.flush();
    }

    @Override
    public Timeout timeout() {
        return sink.timeout();
    }

    @Override
    public void close() throws IOException {
        sink.close();
    }

    @Override
    public long getLoadedBytes() {
        return loadedBytes;
    }

    @Override
    public long getTotalBytes() {
        return totalBytes;
    }
}

public void upload(String url, final String fileName) throws Exception {
	OkHttpClient client = new OkHttpClient.Builder()
			.build();
	//划重点
	RequestBody streamBody = new RequestBody() {
		@Override
		public long contentLength() throws IOException {
			return 100000000;//若是断点续传则返回剩余的字节数
		}
		@Override
		public MediaType contentType() {
			return MediaType.parse("image/png");
			//这个根据上传文件的后缀变化，要是不知道用application/octet-stream
		}
		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			//MultipartBody不让改，否则改造MultipartBody的writeOrCountBytes更好
			//监听当前body的上传进度
			try (BufferedSource source = Okio.buffer(Okio.source(new File(fileName)))) {
				source.skip(102400);//跳到指定位置，断点续传

				//包装sink，划重点
				ProgressSink pSink = new ProgressSink(sink, contentLength());
				ProgressTask task = new ProgressTask(pSink);
				timer.schedule(task, 1000, 1000);//定时读取进度或者
				pSink.writeAll(source);//将文件写到网络中，划重点
				task.cancel();
			}
		}
	};

	RequestBody multiBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("current", "1")
			.addFormDataPart("total", "2")
			.addFormDataPart("file", fileName, streamBody)
			.build();

	Request request = new Request.Builder()
			.url(url)
//			.post(streamBody)
			.post(multiBody)
			.build();

	client.newCall(request).enqueue(new Callback() {//异步
		@Override
		public void onFailure(Call call, IOException e) {}
		@Override
		public void onResponse(Call call, Response response) throws IOException {}
	});
}
```
下载和上传的公共部分：
```
interface Progress {
    public long getLoadedBytes();
    public long getTotalBytes();
}
//定时获取进度
class ProgressTask extends TimerTask {
    private Progress progress;
    private long lastLen = 0;
    private final long contentLen;

    public ProgressTask(Progress progress) {
        this.progress = progress;
        this.contentLen = progress.getTotalBytes();
    }

    @Override
    public void run() {
        long loaded = progress.getLoadedBytes();
        long reciver = loaded - lastLen; //这个在一秒内接收到的数据，可以当作速度
        lastLen = loaded;
        //把速度reciver和进度(lastLen/contentLen)更新到UI

        if (contentLen == loaded) {//下载完了
            this.cancel();
        }
    }
}
```
