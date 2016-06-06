module = angular.module 'Rosemary'

module.directive 'data', ->
  restrict: 'E'
  scope:
    selected: '='
    loadmore: '='
    loading: '='
    data: '='
    final: '='
  templateUrl: 'views/directives/data.html'