/**
 * 新建或修改计划，用于 Controller 的复用
 */
define(function(require, exports, module) {
    module.exports = ['Restangular', 'URL', 'Flash', '$location', function(Restangular, URL, Flash, $location) {

        return {
            // 向 controller 的 scope 注入方法与数据
            initScope: function($scope, planSetting) {
                $scope.planSetting = planSetting;
                $scope.sellerCats = [];

                // 将选中分类的 id 写入 planSetting 对象
                $scope.$watch('sellerCats', function saveCatIds(value) {
                    var selectedCids = _.chain(value).where({selected: true}).pluck('cid').value();
                    $scope.planSetting.includeSellerCids = selectedCids.join(',');
                    $scope.form.includeSellerCids.$dirty = true;
                }, true);

                // 获取卖家的宝贝分类
                Restangular.all(URL.SELLER_CAT).getList().then(function(data) {
                    // 展开第一个有子分类的
                    var firstChild = _.find(data, function(cat) {
                        return cat.parentCid !== 0;
                    });
                    _.findWhere(data, {cid: firstChild.parentCid}).expand = true;

                    // 修改时回填
                    _.each(data, function(cat) {
                        if (_.include($scope.includeSellerCids, cat.cid + '')) {
                            cat.selected = true;
                        }
                    });

                    $scope.sellerCats = data;
                });

                // 分类的父子联动选择
                $scope.changeSelected = function(sellerCat) {
                    // 联动所有子分类
                    if (sellerCat.parentCid === 0) {
                        _.chain($scope.sellerCats).where({parentCid: sellerCat.cid}).each(function(childSellerCat) {
                            childSellerCat.selected = sellerCat.selected;
                        });
                    }
                    // 联动父分类，所有兄弟都选中时才选中父分类
                    else {
                        var parentCat = _.findWhere($scope.sellerCats, {cid: sellerCat.parentCid});
                        var childCats = _.where($scope.sellerCats, {parentCid: sellerCat.parentCid});
                        parentCat.selected = _.every(childCats, function(cat) {
                            return cat.selected;
                        });
                    }
                };

                // 展开或折叠父分类
                $scope.toggleSellerCat = function(sellerCat) {
                    sellerCat.expand = !sellerCat.expand;
                };

                // 保存计划
                $scope.save = function() {
                    if (isValidate()) {
                        var method = $scope.planSetting.id ? 'put' : 'post';
                        var tip = $scope.planSetting.id ? '创建' : '修改';
                        $scope.planSetting[method]().then(function() {
                            Flash.success('计划 ' + $scope.planSetting.name + ' ' + tip + '成功！');
                            $location.url('/plan-setting');
                        });
                    }

                };

                // 跳转至筛选页面
                $scope.gotoFilter = function() {
                    if (isValidate()) {
                        Flash.tmp($scope.planSetting);
                        $location.url('/plan-setting/filter');
                    }
                };

                // 校验表单
                function isValidate() {
                    for (var field in $scope.form) {
                        if (field[0] !== '$' && $scope.form[field].$pristine) {
                            $scope.form[field].$setViewValue($scope.form[field].$modelValue);
                        }
                    }
                    return $scope.form.$valid;
                }
            }
        };
    }];
});