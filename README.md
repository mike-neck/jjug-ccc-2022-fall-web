# jjug-ccc-2022-fall-web
JJUG CCC 2022 Fall の補足用のレポジトリー

1. まずは `make init` を実行してくださいまし！
2. 実行したいプロジェクトの名前を選びますわ！名前はディレクトリーの名前ですわ！
3. プロジェクトの名前を`PROJECT`環境変数に指定して、`run`ターゲットを実行しますわ！

```shell
PROJECT=backend make run
```

環境
---

- M2 Mac book pro
- Mac OS Ventura 13.0.1
- RAM 24GB

実行用ソフトウェア
---

- Java19
- Make 3.81〜
- ab (Apache Bench)
- bash
- curl/grep/tr/unzip

内容
---

- `backend` プロジェクトを起動してくださいまし！

### 前半

I/O呼び出しによるブロッキングとサーバーのスループット性能劣化について説明いたしましたわ！

##### OOMしてしまうthread per request styleサーバー

- `traditional-server` を実行してくださいまし！
- `make test` でテストすると1分ほどで落ちますわよ！

OOMが見たいのではない、性能劣化が見たいのだ！という方は、 `traditional-server` を以下の方法で実行してくださいまし！
サーバーが最大`100`スレッドで起動いたしますわ！このサーバーに対して`make test`すると性能劣化がわかりますわ！

```shell
PROJECT=traditional-server make run-platform
```

詳しくは[こちら](./docs/1-1.oom.md)

##### 非同期スタイルサーバー

- `async-server` を実行してくださいまし！
- `make test` しても性能劣化しませんわ！

##### VirtualThreadを使うthread per request styleサーバー

- `traditional-server` で virtual thread サーバーを実行してくださいまし！
  - `PROJECT=traditional-server make run-virtual` ですわよ！
- `make test` しても性能劣化しませんわ！

### 後半

アプリケーションの移行、PIN診断・回避について説明いたしましたわ！

##### Spring Boot サーバーの移行とPIN

- `spring-boot-2.7.4`の`SpringDbExample`クラスに`VirtualThread`の`ExecutorService`を使った`@Bean`を登録していますわ
- `spring-boot-2.7.4` を実行してくださいまし！
- テスト(`db-app-tester`)を起動してくださいまし！
    - わざと`synchronized`にしている`/api`エンドポイント由来の`PIN`が発生しますわ！

##### PIN回避

- `spring-boot-2.7.4`の`useLock`バージョンを実行してくださいまし！
    - `PROJECT=spring-boot-2.7.4 make run-lock` ですわよ！
- テスト(`db-app-tester`)を起動してくだいまし！
    - `/api` 由来の`PIN`が消えますわ！`java.util.concurrent.Lock`は素晴らしいですわ〜！

##### Spring Boot サーバー3.0.x

Tomcat の `PIN` 回避バージョンが適用されますわ！

- `spring-boot-3` を起動してくださいまし！
- テスト(`db-app-tester`)を起動してくださいまし！
    - Tomcat由来の`PIN`はもう発生しませんわ！楽勝ですわ！
    - 残念ながらmysqlからの`PIN`はまだ発生しますわ！

---

Makefile
---

実行する時は `make` これがあれば間違いありませんわ！

- `init` - `ant`と`ivy`をダウンロードしますわ！
- `new` - 新しいプロジェクトを作りますわ！皆さまが使うことはございませんわ！
- `resolve` - プロジェクトの依存ライブラリーを解決しますわ！
- `idea` - `resolve`で解決したライブラリーを含むIDEAプロジェクトを作りますわ！皆さまが使うことはございませんわ！
- `run` - プロジェクトの依存ライブラリーを解決して、コンパイルして、実行しますわ！
- `clean` - クラスファイルを消しますわ！
- `clean-deps` - 依存ライブラリーを削除しますわ！

参考文献
---

### JEP-425

- https://openjdk.org/jeps/425
- バーチャルスレッドと言えばこれですわ！
- スレッドの効率化いっぱいありますけどもバーチャルスレッドですわ！
- これだけあれば勝ちですわ！
