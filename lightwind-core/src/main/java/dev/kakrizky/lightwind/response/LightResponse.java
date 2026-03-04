package dev.kakrizky.lightwind.response;

public class LightResponse<T> {

    private int code;
    private T data;

    public LightResponse() {}

    public LightResponse(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public static <T> LightResponse<T> ok(T data) {
        return new LightResponse<>(200, data);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
