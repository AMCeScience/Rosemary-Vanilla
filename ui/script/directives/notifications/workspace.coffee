module.directive 'notificationworkspace', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-user bg-yellow"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{ctrl.affected.name}}</span> was {{data.action}} by <span class="title">{{ctrl.author.name}}</span> to the workspace</h4>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  controller: ($scope, $rootScope, $q, $state, Object, Tools, User, Tags, Nav) ->

    new class WorkspaceNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        User.users.all().then =>
          @author = User.users.syncGet @scope.data.actor
          @affected = User.users.syncGet @scope.data.affected

      ###########################
      # Methods                 #
      ###########################

      go: -> $state.go 'main.workspace'