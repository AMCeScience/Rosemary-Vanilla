module.directive 'notificationuserprocessing', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-cog bg-green"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{data.actor.name}}</span> has {{data.action}} processing <span class="title">{{data.processing.name}}</h4>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  controller: ($scope, $rootScope, $q, $state, Object, Remote, User) ->

    new class UserProcessingNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        Remote.one 'processing-groups', @scope.data.processing # NEW_API
        .then (processing) =>
          @scope.data.processing = processing

        User.users.get @scope.data.actor
        .then (user) => @scope.data.actor = user
          
      ###########################
      # Methods                 #
      ###########################