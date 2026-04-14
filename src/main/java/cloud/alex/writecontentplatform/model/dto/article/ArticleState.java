package cloud.alex.writecontentplatform.model.dto.article;

import cloud.alex.writecontentplatform.model.vo.ArticleVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
@Data
public class ArticleState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * taskId
     */
    private String taskId;

    /**
     * 主题
     */
    private String topic;

    /**
     * 风格
     */
    private String style;

    /**
     * 标题结果（智能体1输出）
     */
    private TitleResult title;

    /**
     * 大纲结果（智能体2输出）
     */
    private OutlineResult outline;

    /**
     * 正文内容（智能体3输出）
     */
    private String content;

    /**
     * 配图需求列表（智能体4输出）
     */
    private List<ImageRequirement> imageRequirements;

    /**
     * 封面图URL（单独存储，images的第一张图就是封面图）
     */
    private String coverImage;

    /**
     * 配图结果列表（智能体5输出）
     */
    private List<ImageResult> images;

    /**
     * 完整图文内容（整合后）
     */
    private String fullContent;

    /**
     * 用户补充描述
     */
    private String userDescription;

    /**
     * 当前阶段
     */
    private String phase;

    /**
     *
     */
    private List<TitleOption> titleOptions;

    /**
     * 标题结果
     */
    @Data
    public static class TitleResult implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 大纲结果
     */
    @Data
    public static class OutlineResult implements Serializable {
        private List<OutlineSection> sections;
    }

    /**
     * 大纲章节
     */
    @Data
    public static class OutlineSection implements Serializable {
        private Integer section;
        private String title;
        private List<String> points;
    }

    /**
     * 智能体4的输入
     * 要求生成带有 PLACEHOLDER 的 正文 以及 配图
     */
    @Data
    public static class AddImageRequest implements Serializable {

        /**
         * 带有占位符的正文内容
         */
        private String contentWithPlaceholders;

        /**
         * 配图需求
         */
        private List<ImageRequirement> imageRequirements;
    }

    /**
     * 配图需求
     */
    @Data
    public static class ImageRequirement implements Serializable {
        private Integer position;
        private String type;
        private String sectionTitle;
        private String keywords;
        private String imageSource;
        private String prompt;

        private String placeholderId;
    }

    /**
     * 配图结果
     */
    @Data
    public static class ImageResult implements Serializable {
        private Integer position;
        private String url;
        private String method;
        private String keywords;
        private String sectionTitle;
        private String description;
        private String placeholderId;
    }

    @Data
    public static class TitleOption implements Serializable {
        private String mainTitle;
        private String subTitle;
    }
}
