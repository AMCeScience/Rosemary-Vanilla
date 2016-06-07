# Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
#  
# This program is semi-free software: you can redistribute it and/or modify it
# under the terms of the Rosemary license. You may obtain a copy of this
# license at:
# 
# https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
# You should have received a copy of the Rosemary license
# along with this program. If not, 
# see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
#  
#        Project: https://github.com/AMCeScience/Rosemary-Vanilla
#        AMC eScience Website: http://www.ebioscience.amc.nl/

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