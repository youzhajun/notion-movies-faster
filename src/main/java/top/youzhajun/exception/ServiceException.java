package top.youzhajun.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * exception
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;


    public ServiceException(String message) {
        this.message = message;
    }
}
