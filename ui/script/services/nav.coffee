module = angular.module 'Rosemary'
module.factory 'Nav', ($state, $rootScope, Object) ->

  new class Nav

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    go: (id) ->
      $state.go id

    search: ->
      $state.go 'main.search'

    data: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.type = SEARCH_OPTIONS[3]
      $rootScope.search.tags = if tag? then [ tag ] else []
      $state.go 'main.search'

    datum: (datum) ->
      $state.go 'main.datum',
        workspaceId: Object.toId $rootScope.workspace
        id: Object.toId datum

    processingGroups: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.tags = if tag? then [ tag ] else []
      $rootScope.search.type = SEARCH_OPTIONS[2]
      $state.go 'main.search'

    processings: (tag) ->
      $rootScope.search.query = ""
      $rootScope.search.tags = if tag? then [ tag ] else []
      $rootScope.search.type = SEARCH_OPTIONS[1]
      $state.go 'main.search'

    processingGroup: (group) ->
      $state.go 'main.processingGroup',
        id: Object.toId group

    processing: (processing) ->
      $state.go 'main.processing',
        id: Object.toId processing