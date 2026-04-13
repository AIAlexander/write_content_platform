package cloud.alex.writecontentplatform.common;

/**
 * @author wsh
 * @date 2026/4/4
 */

import cloud.alex.writecontentplatform.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通过响应类
 *
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
