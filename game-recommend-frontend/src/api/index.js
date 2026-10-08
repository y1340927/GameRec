import axios from 'axios';

// ===== 数据分析 API =====
export const getOverview = () => axios.get('/analysis/overview');
export const getRatingDistribution = () => axios.get('/analysis/rating-distribution');
export const getTopActiveUsers = (limit = 20) => axios.get('/analysis/top-active-users', { params: { limit } });
export const getTopHotGames = (limit = 20) => axios.get('/analysis/top-hot-games', { params: { limit } });
export const getTopRatedGames = (limit = 20) => axios.get('/analysis/top-rated-games', { params: { limit } });
export const getPlayHoursDistribution = () => axios.get('/analysis/play-hours-distribution');
export const getPurchasePlayRatio = () => axios.get('/analysis/purchase-play-ratio');

// 新增：游戏评价分析
export const getReviewDistribution = () => axios.get('/analysis/review-distribution');
export const getTopCriticGames = (limit = 20) => axios.get('/analysis/top-critic-games', { params: { limit } });
export const getTopReviewedGames = (limit = 20) => axios.get('/analysis/top-reviewed-games', { params: { limit } });

// 新增：开发商/发行商分析
export const getTopDevelopers = (limit = 20) => axios.get('/analysis/top-developers', { params: { limit } });
export const getTopPublishers = (limit = 20) => axios.get('/analysis/top-publishers', { params: { limit } });
export const getPlatformDistribution = () => axios.get('/analysis/platform-distribution');
export const getPriceDistribution = () => axios.get('/analysis/price-distribution');
export const getTagCloud = () => axios.get('/analysis/tag-cloud');

// 新增：数据质量与趋势
export const getRadarMetrics = () => axios.get('/analysis/radar-metrics');
export const getReleaseYearTrends = (periods = 20) => axios.get('/analysis/release-year-trends', { params: { periods } });
export const getQualityStats = () => axios.get('/analysis/quality-stats');
export const getKpiTrends = (points = 20) => axios.get('/analysis/kpi-trends', { params: { points } });
export const getGenreTrends = (points = 30) => axios.get('/analysis/genre-trends', { params: { points } });

// ===== 画像 API =====
export const getFullProfile = (userId) => axios.get(`/profile/full/${userId}`);
export const getRFM = (userId) => axios.get(`/profile/rfm/${userId}`);
export const getActivity = (userId, decayRate = 0.01) => axios.get(`/profile/activity/${userId}`, { params: { decayRate } });
export const getInterestTags = (userId) => axios.get(`/profile/interest/${userId}`);
export const getPlayerType = (userId) => axios.get(`/profile/player-type/${userId}`);
export const saveAllProfiles = () => axios.post('/profile/save-all');

// ===== 推荐 API =====
export const getUserCF = (userId, topK = 10, topN = 10) => axios.get(`/recommend/user-cf/${userId}`, { params: { topK, topN } });
export const getItemCF = (userId, topN = 10) => axios.get(`/recommend/item-cf/${userId}`, { params: { topN } });
export const buildItemCF = () => axios.post('/recommend/item-cf/build');
export const getContentBased = (userId, topN = 10) => axios.get(`/recommend/content-based/${userId}`, { params: { topN } });
export const getPopular = (topN = 20) => axios.get('/recommend/popular', { params: { topN } });
export const getSVD = (userId, topN = 10) => axios.get(`/recommend/svd/${userId}`, { params: { topN } });
export const trainSVD = (factors = 50, iterations = 20, lr = 0.005, reg = 0.02) =>
  axios.post('/recommend/svd/train', null, { params: { factors, iterations, learningRate: lr, regularization: reg } });
export const getHybrid = (userId, topN = 10) => axios.get(`/recommend/hybrid/${userId}`, { params: { topN } });
export const getRecommend = (userId, topN = 10) => axios.get(`/recommend/${userId}`, { params: { topN } });

// ===== 评估 API =====
export const evaluateAll = (topK = 10) => axios.post('/evaluate/all', null, { params: { topK }, timeout: 300000 });
export const evaluateMultipleK = (topKs = [5, 10, 15, 20]) => axios.post('/evaluate/multiple-k', topKs);
export const getEvalHistory = () => axios.get('/evaluate/history');

// ===== 矩阵 API =====
export const buildMatrix = () => axios.post('/matrix/build');
export const splitTrainTest = () => axios.post('/matrix/split');
export const getMatrixStats = () => axios.get('/matrix/stats');

// ===== 数据清洗 API =====
export const cleanRatings = () => axios.post('/clean/ratings');
export const getCleanStats = () => axios.get('/clean/stats');

// ===== 管理端 API =====
export const listUsers = (page = 1, size = 20, keyword = '') => axios.get('/admin/users', { params: { page, size, keyword } });
export const listGames = (page = 1, size = 20, keyword = '', sortBy = '', tags = '') => axios.get('/admin/games', { params: { page, size, keyword, sortBy, tags } });
export const getGameById = (gameId) => axios.get(`/admin/games/${gameId}`);
export const getGameSuggestions = (keyword, limit = 8) => axios.get('/admin/games/suggestions', { params: { keyword, limit } });
export const listRatings = (page = 1, size = 20, userId = null) => axios.get('/admin/ratings', { params: { page, size, userId } });

// ===== 数据导入 API =====
export const importUsers = (users) => axios.post('/data-import/users', users);
export const importGames = (games) => axios.post('/data-import/games', games);
export const importRatings = (ratings) => axios.post('/data-import/ratings', ratings);
