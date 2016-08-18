package com.harry0000.kancolle.ac

import java.net.URL

import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.chrome.ChromeDriver
import spray.json.DefaultJsonProtocol

case class Place(name: String, address: String)

case class Prefecture(code: Int, name: String, places: Seq[Place])

case class Area(name: String, prefectures: Seq[Prefecture])

trait Crawler {
  private val driver = new ChromeDriver()

  protected def crawl[A](host: String, page: String)(f: => A): A = {
    driver.get(new URL(new URL(host), page).toString)
    val result = f
    driver.quit()
    result
  }

  protected def findElements(by: By): Seq[WebElement] = {
    import scala.collection.JavaConverters._
    driver.findElements(by).asScala
  }

  protected def waiting(timeOutInSeconds: Long): Wait = new Wait(timeOutInSeconds)

  class Wait(timeOutInSeconds: Long) {
    import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}

    def until[A](condition: ExpectedCondition[A]): A = {
      new WebDriverWait(driver, timeOutInSeconds).until(condition)
    }

    def visibilityOf(by: By): WebElement = {
      until(ExpectedConditions.visibilityOfElementLocated(by))
    }

    def invisibilityOf(by: By): Boolean = {
      until(ExpectedConditions.invisibilityOfElementLocated(by))
    }
  }
}

object PlaceCrawler extends Crawler with DefaultJsonProtocol {
  implicit val placeFormat       = jsonFormat2(Place)
  implicit val prefectureFormat  = jsonFormat3(Prefecture)
  implicit val areaFormat        = jsonFormat2(Area)

  private val host = "https://kancolle-arcade.net/"
  private val page = "ac/#/place"
  private val areas = Seq(
    "北海道・東北" -> Seq(
      ( 1, "北海道"),
      ( 2, "青森県"),
      ( 3, "岩手県"),
      ( 4, "宮城県"),
      ( 5, "秋田県"),
      ( 6, "山形県"),
      ( 7, "福島県")
    ),
    "関東" -> Seq(
      ( 8, "茨城県"),
      ( 9, "栃木県"),
      (10, "群馬県"),
      (11, "埼玉県"),
      (12, "千葉県"),
      (13, "東京都"),
      (14, "神奈川県")
    ),
    "中部" -> Seq(
      (15, "新潟県"),
      (16, "富山県"),
      (17, "石川県"),
      (18, "福井県"),
      (19, "山梨県"),
      (20, "長野県"),
      (21, "岐阜県"),
      (22, "静岡県"),
      (23, "愛知県"),
      (24, "三重県")
    ),
    "近畿" -> Seq(
      (25, "滋賀県"),
      (26, "京都府"),
      (27, "大阪府"),
      (28, "兵庫県"),
      (29, "奈良県"),
      (30, "和歌山県")
    ),
    "中国・四国" -> Seq(
      (31, "鳥取県"),
      (32, "島根県"),
      (33, "岡山県"),
      (34, "広島県"),
      (35, "山口県"),
      (36, "徳島県"),
      (37, "香川県"),
      (38, "愛媛県"),
      (39, "高知県")
    ),
    "九州・沖縄" -> Seq(
      (40, "福岡県"),
      (41, "佐賀県"),
      (42, "長崎県"),
      (43, "熊本県"),
      (44, "大分県"),
      (45, "宮崎県"),
      (46, "鹿児島県"),
      (47, "沖縄県")
    )
  )

  def crawl(): Seq[Area] = crawl(host, page) {
    areas.map { case (area, prefectures) =>
      waiting(5L).visibilityOf(By.linkText(area)).click()

      val list =
        prefectures.map { case (code, name) =>
          waiting(5L).visibilityOf(By.linkText(name)).click()
          Prefecture(
            code,
            name,
            findElements(By.cssSelector("li.fc-place-item")).map { e =>
              Place(
                e.findElement(By.cssSelector("div.fc-place-placename")).getText,
                e.findElement(By.cssSelector("div.fc-place-address")).getText
              )
            }
          )
        }

      Area(area, list)
    }.toList
  }
}
