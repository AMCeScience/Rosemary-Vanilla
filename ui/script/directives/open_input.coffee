module = angular.module 'Rosemary'

module.directive 'openInput', ->
  restrict: 'E'
  scope:
    icon: '='
    model: '='
    placeholder: '='
    errors: '='
    type: '@'
  templateUrl: 'views/directives/open_input.html'