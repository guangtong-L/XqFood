-- ============================================================
-- V2: 种子数据（M1 内测用）
-- 20 个常见食材 + 8 道月子菜谱
-- 上线前由运营导入完整 300 道库
-- ============================================================

-- --------- 食材 ---------
INSERT INTO `t_ingredient` (`id`, `name`, `alias`, `category`, `nutrition_per_100g`, `allergen_tags`, `postpartum_taboo`) VALUES
(1001, '番茄',  '["西红柿"]',  '蔬菜', '{"kcal":18,"protein":0.9,"calcium":10,"iron":0.4,"vitC":19}', '[]', 0),
(1002, '鸡蛋',  '["鸡子"]',    '蛋奶', '{"kcal":144,"protein":13.3,"calcium":56,"iron":2.0}',         '["egg"]', 0),
(1003, '白菜',  '["大白菜"]',  '蔬菜', '{"kcal":17,"protein":1.5,"calcium":50,"iron":0.7}',           '[]', 0),
(1004, '胡萝卜','["红萝卜"]',  '蔬菜', '{"kcal":37,"protein":1.0,"calcium":32,"vitA":688}',           '[]', 0),
(1005, '猪蹄',  '["猪手"]',    '肉禽', '{"kcal":260,"protein":22,"calcium":33}',                       '[]', 0),
(1006, '鲫鱼',  '["鲫瓜子"]',  '海鲜', '{"kcal":108,"protein":17.1,"calcium":79}',                     '["seafood"]', 0),
(1007, '红枣',  '["大枣"]',    '水果', '{"kcal":264,"protein":3.2,"iron":2.3,"vitC":14}',              '[]', 0),
(1008, '小米',  '["粟米"]',    '主食', '{"kcal":361,"protein":9,"iron":5.1}',                          '[]', 0),
(1009, '牛腩',  '[]',          '肉禽', '{"kcal":120,"protein":20,"iron":2.8}',                         '[]', 0),
(1010, '香菇',  '["冬菇"]',    '蔬菜', '{"kcal":211,"protein":20,"calcium":83}',                       '[]', 0),
(1011, '木耳',  '["黑木耳"]',  '蔬菜', '{"kcal":205,"protein":12,"iron":97.4}',                        '[]', 0),
(1012, '排骨',  '["猪排骨"]',  '肉禽', '{"kcal":278,"protein":18}',                                    '[]', 0),
(1013, '虾',    '["鲜虾"]',    '海鲜', '{"kcal":87,"protein":18.6,"calcium":62}',                      '["seafood"]', 0),
(1014, '生姜',  '["姜"]',      '调味料','{"kcal":41,"protein":1.3}',                                   '[]', 0),
(1015, '葱花',  '["小葱"]',    '调味料','{"kcal":28,"protein":1.7}',                                   '[]', 0),
(1016, '黑芝麻','[]',          '主食', '{"kcal":531,"protein":19,"calcium":780,"iron":22.7}',          '[]', 0),
(1017, '南瓜',  '["饭瓜"]',    '蔬菜', '{"kcal":22,"protein":0.7,"vitA":148}',                         '[]', 0),
(1018, '土豆',  '["马铃薯"]',  '蔬菜', '{"kcal":76,"protein":2,"vitC":27}',                            '[]', 0),
(1019, '酒酿',  '["米酒"]',    '其他', '{"kcal":140,"protein":1.6,"alcohol":2}',                       '[]', 1),  -- 哺乳期忌
(1020, '辣椒',  '["朝天椒"]',  '调味料','{"kcal":40,"protein":1.4}',                                   '[]', 1); -- 月子忌

-- --------- 食谱 ---------
INSERT INTO `t_recipe` (`id`, `title`, `cover_url`, `cook_minutes`, `difficulty`, `stage_tags`, `nutrition`, `steps`, `description`, `status`) VALUES
(2001, '番茄炒鸡蛋',
 'https://placehold.co/600x400/FF8866/ffffff?text=番茄炒鸡蛋',
 10, 1,
 '["postpartum_early","lactation","weaning"]',
 '{"calories":220,"protein":14,"calcium":80,"iron":2.1}',
 '[{"step":1,"desc":"番茄切块，鸡蛋打散","timer":60},{"step":2,"desc":"热油下蛋液，炒至凝固盛出","timer":120},{"step":3,"desc":"番茄入锅煸出汁，倒入鸡蛋翻炒","timer":180},{"step":4,"desc":"加盐糖调味即可"}]',
 '哺乳期高蛋白家常菜，易消化', 1),

(2002, '红枣小米粥',
 'https://placehold.co/600x400/FF8866/ffffff?text=红枣小米粥',
 30, 1,
 '["postpartum_early","postpartum_late"]',
 '{"calories":180,"protein":4,"iron":3.5}',
 '[{"step":1,"desc":"小米淘洗，红枣去核"},{"step":2,"desc":"冷水下锅大火煮开","timer":300},{"step":3,"desc":"转小火熬 25 分钟至浓稠","timer":1500}]',
 '产后补血暖胃，月子早期首选', 1),

(2003, '鲫鱼豆腐汤',
 'https://placehold.co/600x400/FF8866/ffffff?text=鲫鱼豆腐汤',
 40, 2,
 '["lactation"]',
 '{"calories":190,"protein":20,"calcium":150}',
 '[{"step":1,"desc":"鲫鱼洗净两面煎金黄","timer":480},{"step":2,"desc":"加开水大火煮 15 分钟至奶白","timer":900},{"step":3,"desc":"加豆腐再煮 5 分钟","timer":300},{"step":4,"desc":"撒盐和姜片"}]',
 '经典催乳汤，奶白浓郁', 1),

(2004, '猪蹄黄豆汤',
 'https://placehold.co/600x400/FF8866/ffffff?text=猪蹄黄豆汤',
 90, 2,
 '["lactation"]',
 '{"calories":310,"protein":24,"calcium":120}',
 '[{"step":1,"desc":"猪蹄焯水去血沫"},{"step":2,"desc":"黄豆提前泡发 4 小时"},{"step":3,"desc":"小火慢炖 1.5 小时","timer":5400}]',
 '催乳补蛋白，适合奶水不足', 1),

(2005, '香菇胡萝卜瘦肉粥',
 'https://placehold.co/600x400/FF8866/ffffff?text=香菇胡萝卜瘦肉粥',
 45, 1,
 '["postpartum_late","weaning"]',
 '{"calories":210,"protein":12,"vitA":350}',
 '[{"step":1,"desc":"瘦肉切丁腌 10 分钟"},{"step":2,"desc":"米加水煮成粥底","timer":1500},{"step":3,"desc":"加肉丁、香菇丁、胡萝卜丁再煮 10 分钟","timer":600}]',
 '营养均衡，适合产后恢复期', 1),

(2006, '木耳红枣鸡汤',
 'https://placehold.co/600x400/FF8866/ffffff?text=木耳红枣鸡汤',
 60, 2,
 '["postpartum_early","lactation"]',
 '{"calories":230,"protein":18,"iron":4.5}',
 '[{"step":1,"desc":"鸡焯水"},{"step":2,"desc":"加木耳、红枣慢炖 50 分钟","timer":3000}]',
 '补血暖宫，月子餐黄金搭配', 1),

(2007, '小米南瓜粥',
 'https://placehold.co/600x400/FF8866/ffffff?text=小米南瓜粥',
 25, 1,
 '["postpartum_early","weaning","child"]',
 '{"calories":160,"protein":3,"vitA":220}',
 '[{"step":1,"desc":"南瓜去皮切块","timer":120},{"step":2,"desc":"小米南瓜同煮 20 分钟","timer":1200}]',
 '温润养胃，宝宝辅食也能吃', 1),

(2008, '黑芝麻糊',
 'https://placehold.co/600x400/FF8866/ffffff?text=黑芝麻糊',
 15, 1,
 '["lactation","postpartum_late"]',
 '{"calories":260,"protein":7,"calcium":350}',
 '[{"step":1,"desc":"黑芝麻干锅小火炒香","timer":300},{"step":2,"desc":"打粉冲水或煮成糊","timer":300}]',
 '补钙乌发，下奶又营养', 1);

-- --------- 食谱-食材关联 ---------
INSERT INTO `t_recipe_ingredient` (`id`, `recipe_id`, `ingredient_id`, `quantity`, `is_optional`) VALUES
(3001, 2001, 1001, '2 个',   0),
(3002, 2001, 1002, '3 个',   0),
(3003, 2001, 1015, '少许',   1),

(3004, 2002, 1007, '6 颗',   0),
(3005, 2002, 1008, '100g',   0),

(3006, 2003, 1006, '1 条',   0),
(3007, 2003, 1014, '3 片',   1),

(3008, 2004, 1005, '500g',   0),

(3009, 2005, 1010, '3 朵',   0),
(3010, 2005, 1004, '半根',   0),

(3011, 2006, 1011, '20g',    0),
(3012, 2006, 1007, '6 颗',   0),

(3013, 2007, 1008, '80g',    0),
(3014, 2007, 1017, '200g',   0),

(3015, 2008, 1016, '50g',    0);
