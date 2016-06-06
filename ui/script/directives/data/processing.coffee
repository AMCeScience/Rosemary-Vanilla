module.directive 'dataprocessing', (RecursionHelper) ->
  restrict: 'E'
  templateUrl: 'views/directives/data/processing.html'
  scope:
    selectable: '='
    processing: '='
    color: '@'
  compile: (element, attr) ->
    attr.color = "white" if not attr.color?
    RecursionHelper.compile element
  controller: ($scope, $rootScope, Object, Remote) ->

    new class ProcessingData extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      processing_data: false

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @style = { background: @scope.color }

      getData: =>
        if @processing_data then return
        else @processing_data = true

        ids = 
          _.union @scope.processing.inputs, @scope.processing.outputs
          .filter (d) -> _.has d, 'datum'
          .map (d) -> Object.toId d.datum.datum
        
        Remote.query Object.toId(@root.workspace) + '/data/query/ids', { ids: ids }
        .then (data) =>
          if data.length > 0
            # Build Index
            index = {}
            _.forEach data, (d) -> index[Object.toId d] = d

            _.forEach @scope.processing.inputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum] 

            _.forEach @scope.processing.outputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum]

      childColor: =>
        tinycolor(@scope.color).darken(3).toString()