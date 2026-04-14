package cloud.alex.writecontentplatform.constant;

/**
 * @author wangshuhao
 * @date 2026/4/13
 */
public interface PromptConstant {

    /**
     * 智能体1：生成标题
     */
    String AGENT1_TITLE_PROMPT = """
            你是一位爆款文章标题专家,擅长创作吸引人的标题。
            
            根据以下选题,生成一个爆款文章标题(主标题 + 副标题):
            选题：{topic}
            
            要求:
            1. 主标题要包含数字、情绪化词汇,吸引眼球
            2. 副标题要补充说明,增强吸引力
            3. 标题要简洁有力,不超过30字
            4. 符合新媒体爆款文章的风格
            5. 如果要添加一些符号，如双引号，请添加转移符号，防止json解析失败
            
            请直接返回 JSON 格式,不要有其他内容:
            {
              "mainTitle": "主标题",
              "subTitle": "副标题"
            }
            """;

    /**
     * 智能体2：生成大纲
     */
    String AGENT2_OUTLINE_PROMPT = """
        你是一位专业的文章策划师,擅长设计文章结构。
        
        根据以下标题,生成文章大纲:
        主标题：{mainTitle}
        副标题：{subTitle}
        
        要求:
        1. 大纲要有清晰的逻辑结构
        2. 包含开头引入、核心观点(3-5个)、结尾升华
        3. 每个章节要有明确的标题和核心要点(2-3个)
        4. 适合2000字左右的文章
        5. 如果要添加一些符号，如双引号，请添加转移符号，防止json解析失败
        
        请直接返回 JSON 格式,不要有其他内容:
        {
          "sections": [
            {
              "section": 1,
              "title": "章节标题",
              "points": ["要点1", "要点2，\"要点3\"  "]
            }
          ]
        }
        """;

    /**
     * 智能体3：生成正文
     */
    String AGENT3_CONTENT_PROMPT = """
        你是一位资深的内容创作者,擅长撰写优质文章。
        
        根据以下大纲,创作文章正文:
        主标题：{mainTitle}
        副标题：{subTitle}
        大纲：
        {outline}
        
        要求:
        1. 内容要充实,每个章节300-400字
        2. 语言流畅,富有感染力
        3. 适当使用金句,增强可读性
        4. 添加过渡句,确保逻辑连贯
        5. 使用 Markdown 格式,章节使用 ## 标题
        
        请直接返回 Markdown 格式的正文内容,不要有其他内容。
        """;

    /**
     * 智能体4：分析配图需求
     */
    /**
     * 智能体4：分析配图需求（支持多种图片来源，使用占位符方案）
     */
    String AGENT4_IMAGE_REQUIREMENTS_PROMPT = """
            你是一位专业的新媒体编辑,擅长为文章配图。
            
            根据以下文章内容,分析配图需求,并在正文中插入图片占位符:
            主标题：{mainTitle}
            正文：
            {content}
            
            可用的配图方式：
            PEXELS, ICONIFY
            
            要求:
            1. 识别需要配图的位置(封面、关键章节、段落之间等)
            2. 根据文章内容和结构灵活决定配图数量，避免过多或过少
            3. **在正文中插入占位符**：使用以下两种格式
               - 普通图片占位符：{{IMAGE_PLACEHOLDER_N}}，其中 N 为配图序号（1, 2, 3...），必须独占一行
               - Icon 占位符：{{ICON_PLACEHOLDER_N}}，可以放在文字行内任意位置（用于 ICONIFY 类型）
               - 注意：position=1 的封面图不需要占位符，不要放在正文中
               - 配图占位符可以放在任意合适位置（章节标题后、段落之间、列表项中、文字行内等）
            4. **只能从上述可用的配图方式中选择**, 为每个配图选择最合适的图片来源(imageSource):
               - PEXELS: 适合真实场景、产品照片、人物照片、自然风景等写实图片
               - ICONIFY: 适合图标、符号、小型装饰性图标（如：箭头、勾选、星星、心形等）
            5. 对于 PEXELS 来源: 提供英文搜索关键词(keywords),要准确、具体
            6. 对于 ICONIFY 来源:
               - 识别需要图标的位置（如：列表项标记、步骤指示、重点强调、文字行内装饰等）
               - 可以使用 {{ICON_PLACEHOLDER_N}} 放在文字行内，也可以使用 {{IMAGE_PLACEHOLDER_N}} 独占一行
               - 提供英文图标关键词（keywords），如：check、arrow、star、heart
               - prompt 留空
            7. placeholderId 必须与正文中插入的占位符完全一致
            8. position=1 为封面图
            
            请直接返回 JSON 格式,不要有其他内容:
            {
              "contentWithPlaceholders": "## 章节标题1\\n\\n第一段内容介绍核心概念。\\n\\n{{IMAGE_PLACEHOLDER_1}}\\n\\n第二段深入分析，这里有个图标 {{ICON_PLACEHOLDER_1}} 表示重点。继续详细说明...\\n\\n## 章节标题2\\n\\n第三段阐述应用场景。\\n\\n{{IMAGE_PLACEHOLDER_2}}\\n\\n第四段总结要点。",
              "imageRequirements": [
                {
                  "position": 1,
                  "type": "cover",
                  "sectionTitle": "",
                  "imageSource": "PEXELS",
                  "keywords": "aaa",
                  "prompt": "",
                  "placeholderId": ""
                },
                {
                  "position": 2,
                  "type": "section",
                  "sectionTitle": "章节标题1",
                  "imageSource": "PEXELS",
                  "keywords": "business success teamwork office",
                  "prompt": "",
                  "placeholderId": "{{IMAGE_PLACEHOLDER_1}}"
                },
                {
                  "position": 3,
                  "type": "inline",
                  "sectionTitle": "",
                  "imageSource": "ICONIFY",
                  "keywords": "check circle",
                  "prompt": "",
                  "placeholderId": "{{ICON_PLACEHOLDER_1}}"
                }
              ]
            }
            """;


    // region 文章风格 Prompt

    /**
     * 科技风格 Prompt 附加
     */
    String STYLE_TECH_PROMPT = """
        
        **重要：请使用科技风格进行创作**
        - 语言专业、严谨，多使用专业术语和行业词汇
        - 逻辑清晰，重视数据和事实支撑
        - 叙述客观理性，避免主观情感表达
        - 突出技术创新、发展趋势、解决方案
        - 可适当引用权威资料或专家观点
        """;

    /**
     * 情感风格 Prompt 附加
     */
    String STYLE_EMOTIONAL_PROMPT = """
        
        **重要：请使用情感风格进行创作**
        - 语言温暖细腻，富有感染力和共鸣
        - 善用比喻、排比等修辞手法增强表现力
        - 注重情感表达，讲述真实故事和感悟
        - 引发读者情感共鸣，传递正能量
        - 适当使用抒情语句，增加文章温度
        """;

    /**
     * 教育风格 Prompt 附加
     */
    String STYLE_EDUCATIONAL_PROMPT = """
        
        **重要：请使用教育风格进行创作**
        - 语言通俗易懂，深入浅出地讲解概念
        - 结构清晰，循序渐进，便于学习理解
        - 多用案例、类比帮助读者理解复杂内容
        - 总结重点知识点，提供实用的学习建议
        - 鼓励思考，启发读者自主学习和探索
        """;

    /**
     * 轻松幽默风格 Prompt 附加
     */
    String STYLE_HUMOROUS_PROMPT = """
        
        **重要：请使用轻松幽默风格进行创作**
        - 语言轻松活泼，幽默风趣
        - 善用网络流行语、俏皮话和有趣的比喻
        - 适当自嘲或调侃，增加趣味性
        - 内容轻松易读，让读者在愉快中获取信息
        - 可加入一些有趣的段子或梗，但不失专业性
        """;

    // endregion



}
