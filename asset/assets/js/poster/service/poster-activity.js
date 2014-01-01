/**
 * 海报活动，就是用户创建的海报
 */
define(function(require, exports, module) {
    var paginateResource = require('../../common/paginate-resource');

    module.exports = ['$resource', '$http', function($resource, $http) {
        var URL = '/poster/activities/:id';
        var HTML_URL = URL + '/html';

        var PosterActivity = $resource(URL, {id: '@id'}, {
            query: {
                method: 'GET',
                isArray: true,
                transformResponse: paginateResource.createTransform($http),
                interceptor: {
                    response: paginateResource.responseInterceptor
                }
            },

            saveHTML: {
                method: 'PUT',
                url: HTML_URL
            }
        });

        return PosterActivity;
    }];
});