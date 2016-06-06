module = angular.module 'Rosemary'
module.factory 'Workspace', (Object, User, Tags, Remote) ->

  new class Workspace

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    createWorkspace: (name) ->
      Remote.put 'tags/workspace-tag', # NEW_API
        name: name

    getMembers: (workspace) ->
      User.users.all().then ->
        _.map workspace.rights.members, User.users.syncGet

    addMember: (workspace, user) ->
      Remote.post 'tags/' + Object.toId(workspace) + '/action/add-members', # NEW_API
        userids: [ Object.toId user ]
      .then (data) -> data[0]

    removeMember: (workspace, user) ->
      Remote.post 'tags/' + Object.toId(workspace) + '/action/remove-members', # NEW_API
        userids: [ Object.toId user ]
      .then (data) -> data[0]

    deleteWorkspace: (workspace) ->
      Remote.delete  'tags/' + Object.toId workspace # NEW_API
      .then -> _.remove Tags.sync_workspace_tags, Object.equalFn workspace