module = angular.module 'Rosemary'

module.directive 'measureAndHideArrow', ($timeout) ->  
  (scope, element, attrs) ->
    container = element.parent()
    button = container.find('.btn-more')

    if scope.$last
      scope.$watch container[0].scrollHeight, ->
        if ($(container[0]).innerHeight() >= container[0].scrollHeight)
          button.hide()