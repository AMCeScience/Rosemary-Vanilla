module = angular.module 'Rosemary'

module.directive 'tag', ->
  restrict: 'E'
  scope:
    model: '='
    delete: '='
    click: '='
  templateUrl: 'views/directives/tag.html'
  controller: ($scope, $rootScope, $state, Tags, Auth) ->

    new class Tag extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      tag: {}
      label: ''
      visible: true

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        Tags
        .get $scope.model
        .then (@tag) =>
          if Tags.isWorkspace @tag
            @label = "label-primary"
          else if Tags.isUser @tag
            @label = "label-success"
          else if Tags.isMessage @tag
            @label = "label-info"
          else if Tags.isProcessingCategory @tag
            @label = "label-danger"
          else if Tags.isProcessingStatus @tag
            @label = "label-danger"
          else if Tags.isDatumCategory @tag
            @label = "label-warning"
          else
            @visible = false

      ###########################
      # Methods                 #
      ###########################

      click: (event) =>
        event.stopPropagation()
        
        if @scope.click then @scope.click @scope.model
        else $rootScope.nav.data(@scope.model)