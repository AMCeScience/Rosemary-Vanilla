module.directive 'notificationmessage', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-envelope bg-red"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{ctrl.author.name}} send a message in <span class="title">{{data.info.dict.subject.value}}</span></h4>
        <p ng-if="ctrl.receivers.length > 0"> to {{ctrl.receivers | toSentence}}</p>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  # <p ng-if="data.info.dict.count > 0">
  #   <span class="separator">•</span>
  #   <span ng-if="data.info.dict.countData > 0">
  #     <span class="fa fa-paperclip"></span>
  #     {{data.info.dict.countData}}
  #   </span>
  #   <span ng-if="data.info.dict.countProcessing > 0">
  #     <span class="fa fa-cog"></span>
  #     {{data.info.dict.countProcessing}}
  #   </span>
  # </p>
  controller: ($scope, $rootScope, $q, $state, Object, Tools, User, Tags, Nav) ->

    new class MessageNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @scope.data.info.dict.countData = parseInt @scope.data.info.dict.countData
        @scope.data.info.dict.countProcessing = parseInt @scope.data.info.dict.countProcessing
        @scope.data.info.dict.count = @scope.data.info.dict.countData + @scope.data.info.dict.countProcessing

        $q.all [ User.users.all(), Tags.tags ]
        .then =>
          @author = User.users.syncGet(@scope.data.actor)
          @workspace = Tags.syncGet @scope.data.workspace
          @receivers =
            _ @scope.data.receivers
            .map User.users.syncGet
            .map (user) -> user.name
            .value()

      ###########################
      # Methods                 #
      ###########################

      go: => 
        @root.tmp.thread = @scope.data.thread
        $state.go 'main.communication', {}, { reload: true }