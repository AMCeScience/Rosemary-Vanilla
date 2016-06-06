module = angular.module 'Rosemary'

module.directive 'mainSidebar', ->
  restrict: 'E'
  scope: true
  templateUrl: 'views/directives/main_sidebar.html'
  controller: ($scope, $rootScope, $element, Tags) ->

    new class MainSidebar extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      categories: []

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

      init: =>
        Tags.category_tags.then (@categories) =>

      ###########################
      # Methods                 #
      ###########################