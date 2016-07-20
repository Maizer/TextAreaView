# TextAreaView
Android适合于大文本编辑与显示的TextAreaView
\n优点:
\n1>降低内耗,多行布局内存可降低1/1 - 1/3 之间,随文本大  - 小决定
\n2>单行布局,内存可降低 1/1 - 2/3
\n3>秒速增删改,秒速加载
\n4>支持本地EMOJI
\n5>Cursor/LetterSpace/LineFormat/Ellipsis/Measure等实现高度自定义
\n缺点:
\n1.由于此TextAreaView为格式安全的,当未加载完Text全部起点时,下引速度缓慢
\n2.仅支持部分常用Span和部分特性
\n3.外部选取框与选取按钮需要自己实现
\n
\n由于TextView本身组件的复杂性,可能存在未知BUG,请谅解
