module = angular.module 'Rosemary'

module.directive 'user', ->
  restrict: 'E'
  scope:
    users: '='
  templateUrl: 'views/directives/user.html'

  controller: ($scope, $rootScope, Data, Object, Remote) ->

    new class User extends DefaultController

      ###########################
      # Instance variables      #
      ###########################


      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope


      ###########################
      # Methods                 #
      ###########################

      committee: (user) ->
        @changeRole user, 'committee'

      approve: (user) ->
        @changeRole user, 'approved'

      activate: (user) ->
        @changeRole user, 'active'

      changeRole: (user, role) ->
        Remote.post 'users/' + Object.toId(user) + '/action/change-role',
          role: role
        .then (res) ->
          $scope.$parent.ctrl.load()