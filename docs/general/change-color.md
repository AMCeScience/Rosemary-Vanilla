# How to add a new page in the frontend

Using this tutorial we are going to explain everything that is needed to change the colour of the Rosemary theme.

This tutorial has three steps, starting with changing the general colour.

The available colours are (see `/ui/style/_admin-lte-skins/`):

* [Black](colour-images/black.png)
* [Blue](colour-images/blue.png)
* [Green](colour-images/green.png)
* [Purple](colour-images/purple.png)
* [Red](colour-images/red.png)
* [Yellow](colour-images/yellow.png)

Each colour also has a 'light' version, changing the visuals of the menu:

* Light [Black](colour-images/black-light.png)
* Light [Blue](colour-images/blue-light.png)
* Light [Green](colour-images/green-light.png)
* Light [Purple](colour-images/purple-light.png)
* Light [Red](colour-images/red-light.png)
* Light [Yellow](colour-images/yellow-light.png)

## Step 1

In `/public/views/index.html` change the body class to the desired value, for example:

```html
  <body class="skin-yellow sidebar-mini control-sidebar-open">
```

## Step 2

In `/public/views/directives/activity_sidebar.html` change the sidebar class to the desired value (i.e. dark or light):

```html
  <aside class="control-sidebar control-sidebar-dark visible-lg">
```
or

```html
  <aside class="control-sidebar control-sidebar-light visible-lg">
```

## Step 3

In `/ui/style/app.less` change `@theme-color` to the desired value, possible values are:

* For black: `#000000`
* For blue: `@light-blue`
* For green: `@green`
* For purple: `@purple`
* For red: `@red`
* For yellow: `@yellow`