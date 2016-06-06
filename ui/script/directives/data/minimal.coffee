module.directive 'dataminimal', () ->
  restrict: 'E'
  templateUrl: 'views/directives/data/minimal.html'
  scope:
    datum: '='
  controller: ($scope, $rootScope, Data) ->

    new class MinimalData extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

      ###########################
      # Methods                 #
      ###########################