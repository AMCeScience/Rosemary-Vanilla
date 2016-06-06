module.directive 'datageneric', (RecursionHelper) ->
  restrict: 'E'
  templateUrl: 'views/directives/data/generic.html'
  scope:
    selectable: '='
    datum: '='
    color: '@'
  compile: (element, attr) ->
    attr.color = "white" if not attr.color?
    RecursionHelper.compile element
  controller: ($scope, $rootScope, Data) ->

    new class GenericData extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      children_detail: true

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope
        
        @style = { background: @scope.color }

      ###########################
      # Methods                 #
      ###########################
      
      children: =>
        @children_detail = false
        Data.children @root.workspace, @scope.datum
        .then (@children) =>

      childColor: =>
        tinycolor(@scope.color).darken(3).toString()