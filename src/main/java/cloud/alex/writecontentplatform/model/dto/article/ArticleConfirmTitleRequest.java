package cloud.alex.writecontentplatform.model.dto.article;

import lombok.Data;
import org.apache.ibatis.javassist.SerialVersionUID;

import java.io.Serializable;

/**
 * @author wangshuhao
 * @date 2026/4/14
 */
@Data
public class ArticleConfirmTitleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选中的主标题
     */
    private String selectedMainTitle;

    /**
     * 选中的副标题
     */
    private String selectedSubTitle;

    /**
     * 用户补充的描述
     */
    private String userDescription;



}
