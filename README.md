# Github Tools Plugin for Intellij
Plugin: https://plugins.jetbrains.com/plugin/13366-github-tools

## Elemental features diagram
![Elemental Diagram](https://user-images.githubusercontent.com/4492972/88056345-c6ab2980-cb36-11ea-834e-3b0352a452ed.png)

## Github Login
To use this plugin's basic features you should authenticate with Github using a Github Personal Access Token and your organization's name.

To get your Personal Access Token simply go to your `Settings` -> `Developer settings` -> `Personal access tokens` -> `Generate new token` (https://github.com/settings/tokens)

Check the following permissions needed to read and list repos

![sd](https://i.ibb.co/309vkDw/Screenshot-2020-06-25-at-11-46-42.png)

![sd](https://i.ibb.co/FDrGgps/Screenshot-2020-06-25-at-11-46-46.png)

![sd](https://i.ibb.co/kBHSnzk/Screenshot-2020-06-25-at-11-46-52.png)

## Travis CI Login
To be able to trigger Travis builds from the IDE you need to authenticate with your Travis Personal Access Token

To get one go to your Settings (https://travis-ci.com/account/preferences) and copy the token by clicking `COPY TOKEN` under `API authentication`

Paste it when prompted in the plugin after double clicking "Re-run" on the plugin. The plugin will store it so that you don't need to repeat the process again.

## Circle CI Login
To be able to trigger Circle workflows from the IDE you need to authenticate with your Travis Personal Access Token. **Keep in mind that it should be a Personal token and not the organization one**

To get one go to your Settings (https://app.circleci.com/settings/user/tokens) -> `Personal API Tokens` -> `Create New Token` -> Enter a name for it and copy it!

Paste it when prompted in the plugin after double clicking "Re-run" on the plugin. The plugin will store it so that you don't need to repeat the process again.
