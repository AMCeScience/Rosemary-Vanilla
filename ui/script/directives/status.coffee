module = angular.module 'Rosemary'

module.directive 'status', ->
  restrict: 'E'
  scope:
    statuses: '='
  templateUrl: 'views/directives/status.html'