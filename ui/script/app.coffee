_.mixin s.exports()
_.templateSettings.interpolate = /{{([\s\S]+?)}}/g

# Create Angular App
app = angular.module 'Rosemary', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ui.router',
  'ui.bootstrap',
  'infinite-scroll',
  'RecursionHelper'
]

# Enable external use of angular injector
injector = null

# Helper function
$$ = (services...) ->
  _.map services, injector.get

# Default delay setting for Infinite Scroll
angular.module('infinite-scroll').value 'THROTTLE_MILLISECONDS', 2000

# Start basic application config
app.config ($stateProvider, $urlRouterProvider, $httpProvider) ->
  # Default URL
  $urlRouterProvider.otherwise '/search'

  # Enable JSON payload for HTTP DELETE
  $httpProvider.defaults.headers.delete = 
    "Content-Type": "application/json;charset=utf-8"

  # All routes
  $stateProvider
    # Main unauthorized
    .state 'open',
      templateUrl: "views/open.html"
      controller: "OpenController"
      abstract: true
    .state 'open.register',
      url: "/register"
      templateUrl: "views/register.html"
      controller: "RegisterController"
    .state 'open.login',
      url: "/login"
      templateUrl: "views/login.html"
      controller: "LoginController"
    # Main authorized
    .state 'main',
      templateUrl: "views/main.html"
      controller: "MainController"
      abstract: true
    .state 'main.search',
      url: "/search"
      templateUrl: "views/search.html"
      controller: "SearchController"
    .state 'main.datum',
      url: "/data/:id"
      templateUrl: "views/datum.html"
      controller: "DatumController"
    .state 'main.processingGroup',
      url: "/processing-group/:id"
      templateUrl: "views/processingGroup.html"
      controller: "ProcessingGroupController"
    .state 'main.processing',
      url: "/processing/:id"
      templateUrl: "views/processing.html"
      controller: "ProcessingController"
    .state 'main.process',
      url: "/process"
      templateUrl: "views/process.html"
      controller: "ProcessController"
    .state 'main.communication',
      url: "/communication"
      templateUrl: "views/communication.html"
      controller: "CommunicationController"
    .state 'main.new_message',
      url: "/communication/new"
      templateUrl: "views/new_message.html"
      controller: "NewMessageController"
    .state 'main.workspace',
      url: "/workspace"
      templateUrl: "views/workspace.html"
      controller: "WorkspaceController"
    .state 'main.import',
      url: "/import"
      templateUrl: "views/import.html"
      controller: "ImportController"
    .state 'main.users',
      url: "/users",
      templateUrl: "views/users.html"
      controller: "UsersController"

    # Set some default actions based on HTTP results
    $httpProvider.interceptors.push ($rootScope, $q, $timeout, $injector, Auth) ->
      'responseError': (response) ->
        if response.status is 401
          Auth.logout "Invalid user session"

        else if response.status is 404
          $rootScope.$emit 'alert', new Alert("danger", "<b>404</b> Unknown <i>#{response.config.method}</i> request: #{response.config.url}")
        else if response.status is 409
          $rootScope.$emit 'alert', new Alert("warning", "<b>Conflict</b> #{response.data}")
        else
          $rootScope.$emit 'alert', new Alert("danger", "<b>Unknown error</b> #{response.statusText} | #{JSON.stringify(response.data)}")
        $q.reject response

app.run ($rootScope, $location, $state, $injector, Auth, Root) ->
  # Make the injector global (so external classes can access)
  injector = $injector
  # On every state/page change
  $rootScope.$on "$stateChangeStart", (event, to) ->
    # Reset breadcrumb every page-chrange
    $rootScope.breadcrumb = []

    # Interceptor to ensure only pages starting with the word open can be used without authentication
    if not ( _.startsWith(to.name, 'open') or Auth.isLoggedIn() )
      event.preventDefault()
      $state.go 'open.login'
