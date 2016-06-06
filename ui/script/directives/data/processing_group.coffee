module.directive 'dataprocessinggroup', (RecursionHelper) ->
  restrict: 'E'
  templateUrl: 'views/directives/data/processing_group.html'
  scope:
    selectable: '='
    processinggroup: '='
    color: '@'
  compile: (element, attr) ->
    attr.color = "white" if not attr.color?
    RecursionHelper.compile element
  controller: ($scope, $rootScope, Remote, Object) ->

    new class ProcessingGroupData extends DefaultController

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

        @getData()

      ###########################
      # Methods                 #
      ###########################
      
      getData: =>
        ids = 
          _.union @scope.processinggroup.inputs, @scope.processinggroup.outputs
          .filter (d) -> _.has d, 'datum'
          .map (d) -> Object.toId d.datum.datum
        
        Remote.query Object.toId(@root.workspace) + '/data/query/ids', { ids: ids }
        .then (data) =>
          if data.length > 0
            # Build Index
            index = {}
            _.forEach data, (d) -> index[Object.toId d] = d

            _.forEach @scope.processinggroup.inputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum] 

            _.forEach @scope.processinggroup.outputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum]

      getChildren: =>
        if @processing_data then return
        else @processing_data = true

        Remote.get 'processing-groups/' + Object.toId(@scope.processinggroup) + '/children'
        .then (data) =>
          @scope.processinggroup.processings = data

      childColor: =>
        tinycolor(@scope.color).darken(3).toString()