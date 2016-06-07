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

MODE_DEFAULT = "default"
MODE_TAGGING = "tagging"
MODE_MESSAGE = "message"

module.directive 'basket', ->
  restrict: 'E'
  templateUrl: 'views/directives/basket.html'
  scope:
    mode: '='
    data: '='
  controller: ($scope, $rootScope, $state, Object, Tags) ->

    new class Basket extends DefaultController

      ###########################
      # Instance variables      #
      ###########################
      
      category_selected: []

      basket_tag_model: ""
      basket_tag_suggestions: []

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @mode = @scope.mode

        @root.$watch "basket", @setSelectedCategories
        @scope.$watch "mode", => @mode

        $rootScope.$on 'tag_change', =>
          @initBloodhound()

        @initBloodhound()

      ###########################
      # Methods                 #
      ###########################

      initBloodhound: =>
        Tags.bloodhound_user().then (bloodhound) =>
          @scope.$watch "ctrl.basket_tag_model", (query) =>
            bloodhound.get query, (@basket_tag_suggestions) =>

      setSelectedCategories: =>
        Tags.tags.then (tags) =>
          @category_selected = _ @root.basket.map (item) ->
            switch Object.toType item
              when 'datum'
                category = _.find tags, (tag) ->
                  if Tags.isDatumCategory tag
                    Tags.dataHasTag item, tag

                if category?
                  return category.name
                else 
                  return 'Unmapped'
              when 'processing'
                PROCESSING_NAME
              when 'processing_group'
                PROCESSING_GROUP_NAME
                
          .groupBy (key) -> key
          .transform (result, group) -> result[group[0]] = group.length
          .value()

      sendData: =>
        @root.message.attached = true
        $state.go 'main.new_message'

      tag_create: =>
        Tags.create @basket_tag_model, @tag

      tag: (tag) =>
        Tags.tag tag, @root.basket, =>
          _.forEach @root.basket, (item) ->
            item.tags.push tag
          @mode = MODE_DEFAULT