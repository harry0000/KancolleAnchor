package com.harry0000.kancolle.ac

import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import spray.json.DefaultJsonProtocol

import scala.collection.JavaConverters._

case class Place(name: String, address: String)

case class Prefecture(code: Int, name: String, places: Seq[Place])

case class Area(name: String, prefectures: Seq[Prefecture])

object PlaceCrawler extends DefaultJsonProtocol {
  implicit val placeFormat       = jsonFormat2(Place)
  implicit val prefectureFormat  = jsonFormat3(Prefecture)
  implicit val areaFormat        = jsonFormat2(Area)

  implicit private val driver = new ChromeDriver()

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

  def crawl(): Seq[Area] = {
    driver.get(host + page)

    val s =
      areas.map { case (area, prefectures) =>
        Wait(5).visibilityOf(By.linkText(area)).click()

        val list =
          prefectures.map { case (code, name) =>
            Wait(5).visibilityOf(By.linkText(name)).click()
            Prefecture(
              code,
              name,
              driver.findElements(By.cssSelector("li.fc-place-item")).asScala.map { e =>
                Place(
                  e.findElement(By.cssSelector("div.fc-place-placename")).getText,
                  e.findElement(By.cssSelector("div.fc-place-address")).getText
                )
              }
            )
          }

        Area(area, list)
      }.toList

    driver.quit()

    s
  }

  private final case class Wait(seconds: Int) {
    import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
    import org.openqa.selenium.{WebDriver, WebElement}

    def until[A](condition: ExpectedCondition[A])(implicit driver: WebDriver): A = {
      new WebDriverWait(driver, seconds).until(condition)
    }

    def visibilityOf(by: By)(implicit driver: WebDriver): WebElement = {
      until(ExpectedConditions.visibilityOfElementLocated(by))
    }

    def invisibilityOf(by: By)(implicit driver: WebDriver): Boolean = {
      until(ExpectedConditions.invisibilityOfElementLocated(by))
    }
  }
}
