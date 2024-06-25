import { UIStarterPage } from './app.po';

describe('optio3-web', function() {
  let page: UIStarterPage;

  beforeEach(() => {
    page = new UIStarterPage();
  });

  it('should display page', () => {
    page.navigateTo();
  });
});
