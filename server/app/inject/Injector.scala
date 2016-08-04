package inject

import arcade.{UID, UUID}
import com.google.inject.{AbstractModule, Guice}
import time.{ClockProvider, SystemClockProvider}

trait Injector {
  protected val injector = Guice.createInjector(new KanColleAnchorModule)
}

trait UIDInjector extends Injector {
  protected val uid = injector.getInstance(classOf[UID])
}

private[inject] class KanColleAnchorModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[UID]).toInstance(UUID)
    bind(classOf[ClockProvider]).toInstance(SystemClockProvider)
  }

}
