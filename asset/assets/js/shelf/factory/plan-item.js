/**
 * 计划对应的宝贝
 */
define(function(require, exports, module) {
    module.exports = {
        'PlanItem': ['$resource', 'PaginateUtil', function($resource, PaginateUtil) {
            var URL = '/shelf/plan-settings/:id/items/:numIid';
            var EXCLUDE_URL = URL + '/exclude';

            var PlanItem = $resource(URL, {numIid: '@numIid'}, {
                query: {
                    method: 'GET',
                    isArray: true,
                    transformResponse: PaginateUtil.createTransform(),
                    interceptor: {
                        response: PaginateUtil.responseInterceptor
                    }
                },
                exclude: {
                    method: 'POST',
                    url: EXCLUDE_URL
                },
                include: {
                    method: 'DELETE',
                    url: EXCLUDE_URL
                }
            });

            return PlanItem;
        }]
    };
});