module = angular.module 'Rosemary'
module.controller 'ImportController', ($scope, $rootScope, Remote, $state, Object, Auth) ->

  new class ImportCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    resources: []
    resource: null
    projects: []
    project: null

    loading: false

    changes: []

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

      @root.breadcrumb = [ 'Import' ]

    init: =>
      Remote.all 'resources' # NEW_API
      .then (@resources) => 

    ###########################
    # Methods                 #
    ###########################

    selectResource: (@resource) =>
      @loading = true
      @next()

      get = =>
        Remote
        .post 'import/get-projects',
          resourceid: Object.toId @resource
        .then (@projects) =>
          @loading = false
  
      if @resource.changed
        Remote
        .post 'users/' + Auth.getUserId() + '/action/add-credential', 
          resource: Object.toId @resource
          username: @resource.username
          password: @resource.password
        .then get
      else get()

    selectproject: (@project) =>
      @next()

    import: ->
      Remote
      .post 'import/get-data',
        resourceid: Object.toId @resource
        workspace: Object.toId @root.workspace
        projecturi: @project.pathOnResource
      .then ->
        $rootScope.nav.data()

    change: (resource) -> resource.changed = true