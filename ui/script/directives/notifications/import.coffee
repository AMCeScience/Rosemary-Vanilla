module.directive 'notificationimport', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-cloud-download bg-light-blue"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">Import complete</h4>
        <p>{{ctrl.counts | plural | toSentence | capitalize}} have been imported</p>
        <p>{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  # <div class="desc">{{ctrl.counts | plural | toSentence | capitalize}} have been imported</div>
  #       <span class="separator">•</span>
  #       <span class="name">{{ctrl.author.name}}</span>
  controller: ($scope, $rootScope, $q, Object, Tools, User, Tags, Nav) ->

    new class ImportNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @counts = _ @scope.data.info.dict
        .pick (value, key) -> _.startsWith key, "count"
        .transform (result, value, key) -> result[key.substring 5] = parseInt value.value
        .pick (value, key) -> value > 0
        .value()

        User.users.all().then =>
          @author = User.users.syncGet(@scope.data.actor)

      ###########################
      # Methods                 #
      ###########################

      go: => Nav.data @scope.data.imported