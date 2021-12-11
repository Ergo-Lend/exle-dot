package services

import ergotools.client.Client

import javax.inject.Inject

class StartupService @Inject()(client: Client) {
  println("App Started")
  client.setClient()
}
