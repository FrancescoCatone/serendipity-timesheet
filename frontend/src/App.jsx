import { useLocation } from 'react-router-dom'
import AppRouter from './routes/AppRouter'
import MainLayout from './layout/MainLayout'

export default function App() {
  const isLogin = useLocation().pathname === '/login'
  if (isLogin) return <AppRouter />
  return (
    <MainLayout>
      <AppRouter />
    </MainLayout>
  )
}
