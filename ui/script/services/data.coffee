module = angular.module 'Rosemary'
module.factory 'Data', (Remote, Object) ->

  new class Data

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @resources = new RemoteCollection -> Remote.all 'resources' # NEW_API

    ###########################
    # Methods                 #
    ###########################

    parents: (workspace, id) ->
      Remote.all Object.toId(workspace) + '/data/' + Object.toId(id) + '/parent'

    children: (workspace, id) ->
      Remote.all Object.toId(workspace) + '/data/' + Object.toId(id) + '/children' # NEW_API  