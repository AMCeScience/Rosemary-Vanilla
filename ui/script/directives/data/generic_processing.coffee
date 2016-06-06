module.directive 'genericprocessing', () ->
  restrict: 'E'
  templateUrl: 'views/directives/data/generic_processing.html'
  scope:
    processing: '='
  controller: ($scope, $rootScope) ->

    new class GenericData extends DefaultController

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