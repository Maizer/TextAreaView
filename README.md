![image](https://github.com/Maizer/TextAreaView/blob/TextAreaView/TextAreaViewTest.gif ) 

优点:  

1>降低内耗,多行布局内存可降低1/1 - 1/3 之间,随文本大  - 小决定  

2>单行布局,内存可降低 1/1 - 2/3  

3>秒速增删改,秒速加载  

4>支持本地EMOJI  

5>Cursor/LetterSpace/LineFormat/Ellipsis/Measure等实现高度自定义  

缺点:  

1.由于此TextAreaView为格式安全的,当未加载完Text全部起点时,下引速度缓慢  

2.仅支持部分常用Span和部分特性  

3.外部选取框与选取按钮需要自己实现  

  
由于TextView本身组件的复杂性,可能存在未知BUG,请谅解

Last Repair BUG:

1.ForamtLayout : Repair Measure Background Thread Died Lock.Current background Measure Line Infors will quick Finish.
2.TextInputconnecter : Repair dispatchKeyEvent(KeyEvent e) Bug ,iteration dispatch as before,current Not occur this.
3.TextAreaView :Repair init Cursor ,location offset bug.



