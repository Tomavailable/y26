package com.example.data.dict

import com.example.data.model.WordEntity

data class DictEntry(
    val word: String,
    val phonetic: String,
    val pos: String,
    val meaning: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val notes: String = "",
    val definition: String = "",
    val exchange: String = "",
    val originalMeaning: String = ""
)

object LocalDictionary {

    private val DICT_MAP = mutableMapOf<String, DictEntry>()

    init {
        // Populate offline dictionary with high-frequency academic & daily words
        val entries = listOf(
            DictEntry(
                "abandon", "/əˈbændən/", "vt.",
                "放弃；遗弃；中止",
                "Due to the fierce blizzard, the rescue team had to abandon the mission.",
                "由于凶猛的暴风雪，搜救队不得不放弃了任务。",
                "【词根】a- (离开) + bandon (禁令/控制) -> 摆脱控制 -> 放任，放弃"
            ),
            DictEntry(
                "abundant", "/əˈbʌndənt/", "adj.",
                "大量的；充足的；丰富的",
                "The subtropical coastal island enjoys abundant rainfall and sunshine.",
                "这座亚热带沿海岛屿雨量充沛，日照充足。",
                "【词根】ab- (离开/溢出) + und (波浪) + -ant -> 像水浪般溢出 -> 丰富充足的"
            ),
            DictEntry(
                "accommodate", "/əˈkɒmədeɪt/", "vt.",
                "容纳；顺应；提供住宿",
                "The modern stadium can easily accommodate more than sixty thousand spectators.",
                "这座现代化的体育场馆可轻松容纳六万余名观众。",
                "【词根】ac- (加强) + com (共同) + mod (度量/适合) -> 使彼此适应"
            ),
            DictEntry(
                "accumulate", "/əˈkjuːmjəleɪt/", "v.",
                "积累；积聚；堆积",
                "He accumulated a vast amount of invaluable experience during his years abroad.",
                "他在海外的那些年里积累了大量无比宝贵的经验。",
                "【词根】ac- (加强) + cumul (堆积) + -ate -> 不断累积"
            ),
            DictEntry(
                "adjacent", "/əˈdʒeɪsnt/", "adj.",
                "邻近的；毗连的",
                "The new high-tech research facility is adjacent to the state university campus.",
                "新建立的高科技研发中心与州立大学的校园紧密毗邻。",
                "【词根】ad- (向着) + jac (投/掷/躺) + -ent -> 躺在旁边的 -> 邻近的"
            ),
            DictEntry(
                "advocate", "/ˈædvəkeɪt/", "vt. & n.",
                "主张；提倡；拥护者",
                "Leading environmentalists passionately advocate for sustainable agricultural methods.",
                "知名环保学者们热情地提倡并呼吁采用可持续农业方案。",
                "【词根】ad- (去) + voc (声音/召唤) + -ate -> 发声呼吁支持"
            ),
            DictEntry(
                "aesthetic", "/iːsˈθetɪk/", "adj. & n.",
                "审美的；美学的；艺术的",
                "The architect balanced practical functionality with striking aesthetic appeal.",
                "该建筑设计师巧妙地在实用功能与引人注目的美学吸引力之间实现了平衡。",
                "【词根】aesthet (感知/感觉) + -ic -> 关于感官之美的主张"
            ),
            DictEntry(
                "ambiguous", "/æmˈbɪɡjuəs/", "adj.",
                "模棱两可的；含糊不清的",
                "His deliberately ambiguous reply left the committee members completely confused.",
                "他那番模棱两可、含糊其辞的答复让委员会成员们一头雾水。",
                "【词根】ambi- (两个/周围) + ag (驱使/做) + -uous -> 两头摇摆 -> 含糊不清"
            ),
            DictEntry(
                "anticipate", "/ænˈtɪsɪpeɪt/", "vt.",
                "预料；预期；期盼",
                "Financial analysts anticipate steady economic recovery in the fourth quarter.",
                "金融分析师们预计第四季度经济将迎来稳步复苏。",
                "【词根】anti- (在前) + cip (抓取) + -ate -> 提前预见抓获"
            ),
            DictEntry(
                "apparatus", "/ˌæpəˈreɪtəs/", "n.",
                "器械；设备；机构",
                "The laboratory was newly equipped with modern astronomical research apparatus.",
                "该实验室新近配备了一整套尖端的天文科研仪器设备。",
                "【词根】ap- (朝向) + parat (准备) + -us -> 为特定目的准备的器具"
            ),
            DictEntry(
                "arbitrary", "/ˈɑːbɪtrəri/", "adj.",
                "专断的；随意的；武断的",
                "The manager's decision was criticized as arbitrary and completely unreasonable.",
                "管理层做出的这项裁定被指责过于专横武断且毫无道理。",
                "【词根】arbitr (判断/公断) + -ary -> 单凭个人主观决断"
            ),
            DictEntry(
                "authentic", "/ɔːˈθentɪk/", "adj.",
                "真实的；正宗的；原汁原味的",
                "We tasted authentic Mediterranean cuisine prepared by native culinary masters.",
                "我们品尝了由当地烹饪大师亲自制作的地道正宗地中海料理。",
                "【词根】auto (自己) + hent (做者) -> 亲手原作的 -> 真实的，地道的"
            ),
            DictEntry(
                "benchmark", "/ˈbentʃmɑːk/", "n.",
                "基准；参照点；标杆",
                "This innovative energy efficiency standard sets a new benchmark for the sector.",
                "这项开创性能效规范为整个行业树立了全新的评价标杆。",
                "【构成】bench (台架) + mark (标记) -> 凿在工作台上的测量基准标记"
            ),
            DictEntry(
                "catalyst", "/ˈkætəlɪst/", "n.",
                "催化剂；促成变化的事物或人",
                "The widespread adoption of mobile payment acted as a catalyst for local commerce.",
                "移动支付的全面普及成为了推动本地商业加速升级的强劲催化剂。",
                "【词根】cata- (向下/离开) + lys (解开/溶解) -> 加速反应进程的介质"
            ),
            DictEntry(
                "coincide", "/ˌkəʊɪnˈsaɪd/", "vi.",
                "巧合；同时发生；意见一致",
                "The launch of the flagship phone was scheduled to coincide with their anniversary.",
                "这款旗舰手机的发布会恰好安排在公司成立周年庆典当日举行。",
                "【词根】co- (共同) + in- (上) + cid (落/发生) -> 落在同一个节点上"
            ),
            DictEntry(
                "comprehensive", "/ˌkɒmprɪˈhensɪv/", "adj.",
                "全面的；综合的；详尽的",
                "The university published a comprehensive guide covering all campus services.",
                "大学发布了一份涵盖全校所有服务设施的综合性全景指南。",
                "【词根】com- (全部) + prehend (抓住) + -sive -> 全部包揽进来的"
            ),
            DictEntry(
                "conspicuous", "/kənˈspɪkjuəs/", "adj.",
                "显眼的；明显的；引人注目的",
                "Her brightly colored scarf made her conspicuous even in the dense crowd.",
                "她那条色彩鲜艳的围巾使她在熙熙攘攘的人群中格外显眼引人注目。",
                "【词根】con- (加强) + spic (看) + -uous -> 一眼就能看到的"
            ),
            DictEntry(
                "contemplate", "/ˈkɒntəmpleɪt/", "v.",
                "深思；盘算；沉思凝视",
                "He sat quietly by the serene lake to contemplate his future career path.",
                "他静静地坐在幽静的湖畔，深思沉虑自己未来的职业生涯抉择。",
                "【词根】con- (共同) + templ (神庙/划分之所) + -ate -> 在神庙中占卜沉思"
            ),
            DictEntry(
                "crucial", "/ˈkruːʃl/", "adj.",
                "至关重要的；决定性的",
                "Prompt emergency medical intervention is crucial during the first golden hour.",
                "在黄金急救的前一小时内，迅速果断的医疗救治是至关重要的。",
                "【词根】cruc (十字路口/决定关键) + -ial -> 关键十字路口的抉择"
            ),
            DictEntry(
                "dedicate", "/ˈdedɪkeɪt/", "vt.",
                "奉献；把…献给；题献",
                "The retired scientist decided to dedicate her remaining years to educating youth.",
                "这位退休的女科学家决定将自己的余生奉献给青年科学教育事业。",
                "【词根】de- (加强) + dic (说/宣告) + -ate -> 郑重宣告奉献于此"
            ),
            DictEntry(
                "deteriorate", "/dɪˈtɪəriəreɪt/", "v.",
                "恶化；变坏；衰退",
                "Air quality deteriorated markedly following days of industrial stagnation.",
                "在经历了数日的重度工业停滞后，周围空气质量发生了显著恶化。",
                "【词根】deterior (更坏的) + -ate -> 状况变得越来越差"
            ),
            DictEntry(
                "diligent", "/ˈdɪlɪdʒənt/", "adj.",
                "勤勉的；勤奋细致的",
                "Her diligent research attitude earned her deep respect from fellow peers.",
                "她那种勤勉严谨、一丝不苟的科研态度赢得了同行们的由衷敬重。",
                "【词根】di- (分开) + lig (挑选) + -ent -> 精挑细选每件事 -> 勤奋用心的"
            ),
            DictEntry(
                "diminish", "/dɪˈmɪnɪʃ/", "v.",
                "减少；削弱；贬低",
                "The medication helped diminish the intense physical discomfort within an hour.",
                "服药后不到一小时，剧烈的身体不适感便得到了显著减轻。",
                "【词根】di- (向下) + mini (微小) + -ish -> 使之缩小削弱"
            ),
            DictEntry(
                "discreet", "/dɪˈskriːt/", "adj.",
                "谨慎的；言行审慎的；不引人注目的",
                "The diplomat handled the delicate political inquiry in a very discreet manner.",
                "这位资深外交官以极为谨慎、得体的方式处理了那起敏感的政治调查。",
                "【词根】dis- (分开) + creet (辨别) -> 能明察秋毫言语分寸的"
            ),
            DictEntry(
                "distinct", "/dɪˈstɪŋkt/", "adj.",
                "清晰的；截然不同的；独特的",
                "There is a distinct aroma of fresh roasted coffee beans in the bakery.",
                "烘焙坊里弥漫着一股十分明显、独特的现磨新鲜咖啡香气。",
                "【词根】di- (分开) + stinct (刺/刺标) -> 刺以不同标记 -> 区分清晰的"
            ),
            DictEntry(
                "diverse", "/daɪˈvɜːs/", "adj.",
                "多元的；各式各样的；不同的",
                "The metropolis boasts a rich and diverse demographic profile.",
                "这座国际大都市拥有丰富多元的人口族群与文化风貌。",
                "【词根】di- (分开) + vers (转) -> 朝向不同方向转动 -> 多样化的"
            ),
            DictEntry(
                "elaborate", "/ɪˈlæbərət/", "adj. & v.",
                "精致复杂的；详尽阐述",
                "The professor asked the student to elaborate on his thesis conclusion.",
                "教授请该同学对其学位论文的结语核心论据作进一步的详尽阐述。",
                "【词根】e- (出) + labor (劳动) + -ate -> 付出大量辛勤劳动打造的 -> 精雕细琢的"
            ),
            DictEntry(
                "eloquent", "/ˈeləkwənt/", "adj.",
                "雄辩的；有说服力的",
                "She gave an eloquent speech that moved the audience at the conference.",
                "她在研讨会上发表了一篇极富说服力且扣人心弦的精彩演讲。",
                "【词根】e- (出) + loqu (说话) + -ent -> 谈吐流利有力的"
            ),
            DictEntry(
                "elusive", "/ɪˈluːsɪv/", "adj.",
                "难以捉摸的；难找的；不易理解的",
                "A definitive cure for this rare biological condition remains elusive.",
                "针对这种罕见生理病症的根治之法，目前医学界仍然难以确切觅得。",
                "【词根】e- (出) + lus (玩弄/逃避) + -ive -> 擅长闪避躲藏的"
            ),
            DictEntry(
                "emphasis", "/ˈemfəsɪs/", "n.",
                "强调；重点；着重点",
                "The educational reform places great emphasis on critical thinking skills.",
                "本次教学体制改革特别将重点放在了培养学生的独立批判性思维上。",
                "【词根】em- (进入) + phas (显示/言说) -> 在特定处予以鲜明展示"
            ),
            DictEntry(
                "epiphany", "/ɪˈpɪfəni/", "n.",
                "顿悟；灵光乍现；突然领悟",
                "While staring at the mathematical formula, he experienced an unexpected epiphany.",
                "凝视着眼前的数学公式时，他脑海中突然浮现出一阵豁然开朗的领悟。",
                "【词源】epi- (显现) + phan (发光) -> 神迹或智慧之光突然显现"
            ),
            DictEntry(
                "evaporate", "/ɪˈvæpəreɪt/", "v.",
                "蒸发；消散；不复存在",
                "All his doubts seemed to evaporate once the positive clinical results arrived.",
                "当令人振奋的临床报告送达时，他心中的疑虑顷刻间烟消云散。",
                "【词根】e- (出) + vapor (水汽) + -ate -> 化作水汽散去"
            ),
            DictEntry(
                "exquisite", "/ɪkˈskwɪzɪt/", "adj.",
                "精美的；雅致的；强烈的",
                "The bride wore a handcrafted gown adorned with exquisite lace embroidery.",
                "新娘身着一袭缀有精致细腻蕾丝刺绣的手工定制婚纱。",
                "【词根】ex- (出) + quis (寻求/寻找) + -ite -> 精心挑选寻得的极品"
            ),
            DictEntry(
                "feasible", "/ˈfiːzəbl/", "adj.",
                "可行的；行得通的；做得到的",
                "Engineers determined that the deep subway tunnel project was technically feasible.",
                "资深工程师团队一致认定该深层地铁隧道项目在技术层面上完全可行。",
                "【词根】feas (做/造) + -ible -> 可以办到的"
            ),
            DictEntry(
                "genuine", "/ˈdʒenjuɪn/", "adj.",
                "真诚的；真实的；非伪造的",
                "He expressed genuine sympathy for the disaster-affected families.",
                "他向受到自然灾害重创的受难家庭表达了最真挚由衷的慰问。",
                "【词根】gen (出生/产生) + -uine -> 天生本真的，不虚伪的"
            ),
            DictEntry(
                "horizon", "/həˈraɪzn/", "n.",
                "地平线；眼界；视野",
                "Traveling across multiple continents helped broaden his cultural horizons.",
                "在多大洲之间漫步游历，极大地开阔了他的全球文化视野与胸襟。",
                "【词根】horiz (边界) + -on -> 视线尽头的界线"
            ),
            DictEntry(
                "illuminate", "/ɪˈluːmɪneɪt/", "vt.",
                "照亮；阐明；启发",
                "The historical documentary helped illuminate complex international conflicts.",
                "这部深入的历史纪录片有力地阐明了错综复杂的国际地缘冲突渊源。",
                "【词根】il- (进入) + lumin (光) + -ate -> 注入光芒 -> 照亮，阐述清楚"
            ),
            DictEntry(
                "indispensable", "/ˌɪndɪˈspensəbl/", "adj.",
                "必不可少的；不可或缺的",
                "Smartphone navigation has become indispensable in our daily travel.",
                "智能手机地图导航在当今人们的日常出行中早已成为不可或缺的工具。",
                "【词根】in- (不) + dispensable (可分配出去/可免除的) -> 不能没有的"
            ),
            DictEntry(
                "inevitable", "/ɪnˈevɪtəbl/", "adj.",
                "不可避免的；必然发生的",
                "With rapid urbanization, shifts in urban traffic patterns were inevitable.",
                "伴随着高速的城市化进程，城市交通形态的深刻演变是势在必行的必然结果。",
                "【词根】in- (不) + evit (躲避) + -able -> 无法躲避开的"
            ),
            DictEntry(
                "lucid", "/ˈluːsɪd/", "adj.",
                "清晰明了的；清醒的；表达清楚的",
                "She provided a remarkably lucid explanation of quantum physics basics.",
                "她对量子物理学的基础概念作出了格外通俗、清晰明了的生动解读。",
                "【词根】luc (光) + -id -> 光芒照透的 -> 极其明朗透彻的"
            ),
            DictEntry(
                "mellifluous", "/meˈlɪfluəs/", "adj.",
                "悦耳动听的；声音甜美的",
                "The soprano's mellifluous voice resonated throughout the grand auditorium.",
                "女高音那如流蜜般悦耳甜美的天籁之音在宏伟的音乐大厅中久久回荡。",
                "【词根】melli (蜜) + flu (流动) + -ous -> 声音如流淌的蜂蜜般甘甜"
            ),
            DictEntry(
                "meticulous", "/məˈtɪkjələs/", "adj.",
                "一丝不苟的；缜密周到的",
                "The museum restorer carried out meticulous repair work on the century-old painting.",
                "博物馆文物修复师对这幅有着百年历史的名画进行了极其严谨细致的修复。",
                "【词根】met (测量/敬畏) + -iculous -> 唯恐出错而处处谨慎周全"
            ),
            DictEntry(
                "paradox", "/ˈpærədɒks/", "n.",
                "悖论；自相矛盾的情况",
                "It is a curious paradox that wealth does not always equate to human happiness.",
                "拥有巨额财富往往并不等同于拥有幸福快乐，这真是一个令人玩味的悖论。",
                "【词根】para- (超越/对立) + dox (观点) -> 违反常理却又发人深省的命题"
            ),
            DictEntry(
                "resilience", "/rɪˈzɪliəns/", "n.",
                "韧性；复原力；弹力",
                "The community demonstrated astounding resilience in rebuilding after the typhoon.",
                "全镇居民在台风过后的家园重建工作中展现出了令人惊叹的强大韧性。",
                "【词根】re- (回) + sili (跳跃) + -ence -> 受到重压后立刻弹回原状"
            ),
            DictEntry(
                "serendipity", "/ˌserənˈdɪpəti/", "n.",
                "机缘巧合；意外撞大运",
                "Finding this out-of-print masterpiece was an instance of pure serendipity.",
                "在旧书堆中寻觅到这本绝版名作，真可谓是一次奇妙至极的机缘巧合。",
                "【典故】源于童话《塞伦迪普的三位王子》，指在旅途中总能意外发现美好事物"
            ),
            DictEntry(
                "solitude", "/ˈsɒlɪtjuːd/", "n.",
                "独处；幽居；清静",
                "The poet cherished quiet solitude while crafting verse deep in the mountains.",
                "这位诗人在深山古木间构思诗篇时，格外珍视这份远离喧嚣的清静独处。",
                "【词根】sol (独自) + -itude -> 独自一人的清幽境界"
            ),
            DictEntry(
                "subtle", "/ˈsʌtl/", "adj.",
                "微妙的；敏锐的；不易察觉的",
                "There was a subtle change in the speaker's tone that signaled dissatisfaction.",
                "演讲者语调中掠过一丝极不易察觉的微妙变化，透露出了内心的些许不满。",
                "【词根】sub- (在下) + tle (编织细线) -> 隐于底部的细密纹理"
            ),
            DictEntry(
                "transient", "/ˈtrænziənt/", "adj.",
                "短暂的；转瞬即逝的；过客",
                "The vibrant colors of the sunset were beautiful yet transient.",
                "夕阳余晖那绚丽斑斓的晚霞美不胜收，然而终究只是转瞬即逝的刹那芳华。",
                "【词根】trans- (穿过) + i (走) + -ent -> 匆匆穿行而过的"
            ),
            DictEntry(
                "ubiquitous", "/juːˈbɪkwɪtəs/", "adj.",
                "无处不在的；普遍存在的",
                "Smartphones have become ubiquitous across modern society in just two decades.",
                "短短二十年间，智能手机在现代人类社会的每一个角落已经无处不在。",
                "【词根】ubique (到处) + -itous -> 遍布每个角落的"
            ),
            DictEntry(
                "unprecedented", "/ʌnˈpresɪdentɪd/", "adj.",
                "史无前例的；空前的",
                "The scientific mission achieved an unprecedented degree of international collaboration.",
                "该项科学深空探测任务达成了前所未有、跨国界深度的空前合作规模。",
                "【词根】un- (无) + precedent (先例) + -ed -> 没有先例可循的"
            )
        )

        for (entry in entries) {
            DICT_MAP[entry.word.lowercase().trim()] = entry
        }
    }

    fun lookup(word: String): DictEntry? {
        val clean = word.lowercase().trim()
        val local = DICT_MAP[clean]
        if (local != null) return local
        return CocaFrequencyDictionary.lookup(clean)
    }

    /**
     * Enriches a word entity with offline dictionary definitions,
     * phonetics, POS, example sentences, and translations if they are blank.
     */
    fun enrichWord(word: WordEntity): WordEntity {
        val entry = lookup(word.word) ?: return word

        return word.copy(
            phonetic = word.phonetic.ifBlank { entry.phonetic },
            pos = word.pos.ifBlank { entry.pos },
            meaning = word.meaning.ifBlank { entry.meaning },
            definition = word.definition.ifBlank { entry.definition },
            exampleSentence = word.exampleSentence.ifBlank { entry.exampleSentence },
            exampleTranslation = word.exampleTranslation.ifBlank { entry.exampleTranslation },
            notes = word.notes.ifBlank { entry.notes },
            originalMeaning = word.originalMeaning.ifBlank { entry.originalMeaning.ifBlank { entry.meaning } }
        )
    }

    /**
     * Generates a ready-to-import CSV text of common words for users
     */
    fun generateOfflineDictionaryCsv(): String {
        val sb = StringBuilder()
        sb.append("word,phonetic,meaning,pos,example,translation,notes\n")
        for (e in DICT_MAP.values) {
            sb.append("\"${e.word}\",\"${e.phonetic}\",\"${e.meaning}\",\"${e.pos}\",\"${e.exampleSentence}\",\"${e.exampleTranslation}\",\"${e.notes}\"\n")
        }
        return sb.toString()
    }
}
