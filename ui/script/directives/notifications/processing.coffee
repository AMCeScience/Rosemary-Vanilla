module.directive 'notificationprocessing', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-cog bg-green"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">Processing <span class="title">{{data.processing.name}}</span> has changed its status to <span class="title">{{data.status}}</span></h4>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  controller: ($scope, $rootScope, $q, $state, Remote) ->

    new class ProcessingNotification extends DefaultController

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

      ###########################
      # Methods                 #
      ###########################