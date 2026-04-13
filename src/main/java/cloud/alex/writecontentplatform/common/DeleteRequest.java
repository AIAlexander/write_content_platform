package cloud.alex.writecontentplatform.common;

/**
 * @author wsh
 * @date 2026/4/4
 */

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求包装类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
