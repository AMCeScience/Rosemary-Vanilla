module = angular.module 'Rosemary'
module.controller 'ProcessingGroupController', ($scope, $rootScope, $q, $state, $stateParams, User, Remote, Object, Tags, Auth) ->

  new class ProcessingGroupCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.$watch 'workspace', @getData()
          
    ###########################
    # Methods                 #
    ###########################

    getData: =>
      Remote.get 'processing-groups/' + $state.params.id
      .then (@processing_group) =>
        $rootScope.breadcrumb = [
          'Data'
          'Processing Group'
          @processing_group.name
        ]

        @last_status = _.last(@processing_group.statuses).status

        @getIO()
        @getRecipes()
        @getChildren()

    getChildren: =>
      Remote.get 'processing-groups/' + $state.params.id + '/children'
      .then (@children) =>

    getRecipes: =>
      ids = _.map @processing_group.recipes, (recipe) -> Object.toId recipe
      
      Remote.query 'recipes/query/ids', ids: ids
      .then (data) =>
        if data.length > 0
          # Build Index
          index = {}
          _.forEach data, (d) -> index[Object.toId d] = d

          _.forEach @processing_group.recipes, (recipe, key) =>
            @processing_group.recipes[key] = index[Object.toId recipe]

    getIO: =>
      Remote.post Object.toId(@root.workspace) + '/data/query/ids',
        ids: _.map @processing_group.inputs, (obj) -> Object.toId obj.datum.datum
      .then (@inputs) =>

    abort: =>
      Remote.post 'processing-groups/' + $state.params.id + '/action/abort', {}
      .then (response) =>
        @getData()

    resume: =>
      Remote.post 'processing-groups/' + $state.params.id + '/action/resume', {}
      .then (response) =>
        @getData()