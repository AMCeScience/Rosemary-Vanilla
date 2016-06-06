NOTIFICATION_USER_WORKSPACE = "nl.amc.ebioscience.rosemary.models.UserWorkspaceNotification"
NOTIFICATION_IMPORT = "nl.amc.ebioscience.rosemary.models.ImportNotification"
NOTIFICATION_MESSAGE = "nl.amc.ebioscience.rosemary.models.MessageNotification"
NOTIFICATION_USER_PROCESSING = "nl.amc.ebioscience.rosemary.models.UserProcessingNotification"
NOTIFICATION_PROCESSING = "nl.amc.ebioscience.rosemary.models.ProcessingNotification"

NOTIFICATION_USER_WORKSPACE_TYPE = "user_workspace"
NOTIFICATION_IMPORT_TYPE = "import"
NOTIFICATION_MESSAGE_TYPE = "message"
NOTIFICATION_USER_PROCESSING_TYPE = "user_processing"
NOTIFICATION_PROCESSING_TYPE = "processing"

module = angular.module 'Rosemary'

module.directive 'activitySidebar', ->
  restrict: 'E'
  scope: true
  templateUrl: 'views/directives/activity_sidebar.html'

  controller: ($scope, $rootScope, Object, Remote) ->

    new class ActivitySidebar extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      messages: []
      live: {}

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @root.$watch 'workspace', (workspace) =>
          if _.isObject workspace then @initWithWorkspace()

        @root.$on "websocket:import", (event, data) =>
          if _.has @live, data.id
            @live[data.id].update data
          else
            @live[data.id] = new LiveImport data
          @scope.$apply()

        @root.$on "websocket:notification", (event, data) =>
          if _.some data.tags, Object.equalFn @root.workspace
            @messages.unshift @toMessage data
            @scope.$apply()

      initWithWorkspace: =>
        Remote.query 'notifications/query', # NEW_API
          workspace: Object.toId @root.workspace
        .then (notifications) =>
          @messages = _.map notifications, @toMessage

      ###########################
      # Methods                 #
      ###########################

      toMessage: (notification) ->
        data: notification
        type: switch notification._t
                when NOTIFICATION_USER_WORKSPACE then NOTIFICATION_USER_WORKSPACE_TYPE
                when NOTIFICATION_IMPORT then NOTIFICATION_IMPORT_TYPE
                when NOTIFICATION_MESSAGE then NOTIFICATION_MESSAGE_TYPE
                when NOTIFICATION_USER_PROCESSING then NOTIFICATION_USER_PROCESSING_TYPE
                when NOTIFICATION_PROCESSING then NOTIFICATION_PROCESSING_TYPE

      hasLive: => _.keys(@live).length > 0