package pt.unl.fct.di.adc.firstwebapp.util;

public class SuccessResponse<T> {
    public String status;
    public T data;

    public SuccessResponse() {
    }

    public SuccessResponse(T data) {
        this.status = "success";
        this.data = data;
    }
}
